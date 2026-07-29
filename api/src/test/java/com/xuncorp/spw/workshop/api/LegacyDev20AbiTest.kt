package com.xuncorp.spw.workshop.api

import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Member
import java.lang.reflect.Modifier
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.jar.JarFile

class LegacyDev20AbiTest {
    @Test
    fun `public dev20 binary members remain linkable`() {
        val legacyJar = Path.of(
            requireNotNull(System.getProperty(LEGACY_JAR_PROPERTY)) {
                "Missing $LEGACY_JAR_PROPERTY"
            }
        )
        val currentLoader = LegacyDev20AbiTest::class.java.classLoader
        val failures = mutableListOf<String>()

        LegacyApiClassLoader(legacyJar.toUri().toURL(), currentLoader).use { legacyLoader ->
            JarFile(legacyJar.toFile()).use { jar ->
                val classNames = jar.entries().asSequence()
                    .filter { !it.isDirectory && it.name.endsWith(".class") }
                    .map { it.name.removeSuffix(".class").replace('/', '.') }
                    .filter { it.startsWith(API_PACKAGE_PREFIX) }
                    .filterNot { it == "module-info" }
                    .sorted()
                    .toList()

                classNames.forEach { className ->
                    compareClass(
                        className = className,
                        legacyLoader = legacyLoader,
                        currentLoader = currentLoader,
                        failures = failures
                    )
                }
            }
        }

        assertTrue(
            "Workshop API is not binary compatible with 0.1.0-dev20:\n" +
                failures.joinToString(separator = "\n"),
            failures.isEmpty()
        )
    }

    private fun compareClass(
        className: String,
        legacyLoader: ClassLoader,
        currentLoader: ClassLoader,
        failures: MutableList<String>
    ) {
        val legacyClass = runCatching {
            Class.forName(className, false, legacyLoader)
        }.getOrElse {
            failures += "$className: cannot load legacy class (${it.message})"
            return
        }
        if (!legacyClass.isBinaryApi()) return

        val currentClass = runCatching {
            Class.forName(className, false, currentLoader)
        }.getOrElse {
            failures += "$className: class was removed"
            return
        }

        if (legacyClass.isInterface != currentClass.isInterface) {
            failures += "$className: class/interface kind changed"
        }

        val currentMethods = currentClass.declaredMethods
            .filter { it.isBinaryApi() }
            .mapTo(mutableSetOf(), ::methodSignature)
        legacyClass.declaredMethods
            .filter { it.isBinaryApi() }
            .map(::methodSignature)
            .filterNot(currentMethods::contains)
            .forEach { failures += "$className: method removed or changed: $it" }

        val currentConstructors = currentClass.declaredConstructors
            .filter { it.isBinaryApi() }
            .mapTo(mutableSetOf(), ::constructorSignature)
        legacyClass.declaredConstructors
            .filter { it.isBinaryApi() }
            .map(::constructorSignature)
            .filterNot(currentConstructors::contains)
            .forEach { failures += "$className: constructor removed or changed: $it" }

        val currentFields = currentClass.declaredFields
            .filter { it.isBinaryApi() }
            .mapTo(mutableSetOf()) {
                "${it.name}:${it.type.name}:static=${Modifier.isStatic(it.modifiers)}"
            }
        legacyClass.declaredFields
            .filter { it.isBinaryApi() }
            .map {
                "${it.name}:${it.type.name}:static=${Modifier.isStatic(it.modifiers)}"
            }
            .filterNot(currentFields::contains)
            .forEach { failures += "$className: field removed or changed: $it" }
    }

    private fun Class<*>.isBinaryApi(): Boolean =
        Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)

    private fun Member.isBinaryApi(): Boolean =
        Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)

    private fun methodSignature(method: java.lang.reflect.Method): String =
        "${method.name}(${method.parameterTypes.joinToString { it.name }}):" +
            "${method.returnType.name}:static=${Modifier.isStatic(method.modifiers)}"

    private fun constructorSignature(
        constructor: java.lang.reflect.Constructor<*>
    ): String = "(${constructor.parameterTypes.joinToString { it.name }})"

    private class LegacyApiClassLoader(
        url: URL,
        parent: ClassLoader
    ) : URLClassLoader(arrayOf(url), parent) {
        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            synchronized(getClassLoadingLock(name)) {
                var loaded = findLoadedClass(name)
                if (loaded == null && name.startsWith(API_PACKAGE_PREFIX)) {
                    loaded = runCatching { findClass(name) }.getOrNull()
                }
                if (loaded == null) {
                    loaded = super.loadClass(name, false)
                }
                if (resolve) {
                    resolveClass(loaded)
                }
                return loaded
            }
        }
    }

    private companion object {
        const val LEGACY_JAR_PROPERTY = "spw.workshop.legacyDev20Jar"
        const val API_PACKAGE_PREFIX = "com.xuncorp.spw.workshop.api."
    }
}
