package id.diskola.app.apiservice

import java.lang.reflect.GenericArrayType
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import kotlin.coroutines.Continuation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Retrofit refuses, when the method is first called, any annotated parameter whose generic type holds a wildcard ("Parameter type must not
 * include a type variable or wildcard"). Kotlin writes `Map<String, Any>` in a parameter as `Map<String, ? extends Object>` because `Any` is
 * not final, so such a method throws before a single byte is sent — which is how "Gagal menyimpan nilai" came about. The fix is
 * `Map<String, @JvmSuppressWildcards Any>`; this test finds every method that would still fail, without a device or a network.
 */
class ApiServiceSignatureTest {

    private val services: List<Class<*>> = listOf(
        AgendaApiService::class.java,
        AkunApiService::class.java,
        AsesmenApiService::class.java,
        AuthApiService::class.java,
        CommonApiService::class.java,
        DanaPartisipasiApiService::class.java,
        EntrepreneurApiService::class.java,
        JurnalApiService::class.java,
        KartuPelajarApiService::class.java,
        KlaspayApiService::class.java,
        KonselingApiService::class.java,
        MagangApiService::class.java,
        MateriApiService::class.java,
        NotifikasiApiService::class.java,
        PengumumanApiService::class.java,
        PerpustakaanApiService::class.java,
        PpobApiService::class.java,
        PresensiApiService::class.java,
        ProkesApiService::class.java,
        SosmedApiService::class.java,
        TryOutApiService::class.java,
        TugasApiService::class.java,
        UjianLegacyApiService::class.java,
    )

    /** Same test as Retrofit's `Utils.hasUnresolvableType`. */
    private fun unresolvable(type: Type): Boolean = when (type) {
        is Class<*> -> false
        is ParameterizedType -> type.actualTypeArguments.any { unresolvable(it) }
        is GenericArrayType -> unresolvable(type.genericComponentType)
        is TypeVariable<*>, is WildcardType -> true
        else -> false
    }

    private fun offenders(): List<String> = services.flatMap { service ->
        service.declaredMethods.flatMap { method -> brokenParameters(service, method) }
    }

    private fun brokenParameters(service: Class<*>, method: Method): List<String> =
        method.genericParameterTypes.mapIndexedNotNull { index, type ->
            val annotated = method.parameterAnnotations[index].isNotEmpty()
            val isContinuation = (type as? ParameterizedType)?.rawType == Continuation::class.java
            if (annotated && !isContinuation && unresolvable(type)) "${service.simpleName}.${method.name} (parameter #${index + 1}): $type" else null
        }

    @Test
    fun `every annotated parameter of every API method is something Retrofit accepts`() {
        val found = offenders()
        assertEquals("methods Retrofit would reject:\n" + found.joinToString("\n"), emptyList<String>(), found)
    }

    @Test
    fun `the check itself sees a wildcard when there is one`() {
        // `List<out E>` in a parameter is written with a wildcard unless `E` is final, so this is the shape that must be reported.
        val wildcard = Probe::class.java.declaredMethods.first { it.name == "bodyWithWildcard" }
        val suppressed = Probe::class.java.declaredMethods.first { it.name == "bodySuppressed" }
        assertTrue(brokenParameters(Probe::class.java, wildcard).isNotEmpty())
        assertTrue(brokenParameters(Probe::class.java, suppressed).isEmpty())
    }

    @Suppress("unused")
    private interface Probe {
        fun bodyWithWildcard(@retrofit2.http.Body body: Map<String, Any>)
        fun bodySuppressed(@retrofit2.http.Body body: Map<String, @JvmSuppressWildcards Any>)
    }
}
