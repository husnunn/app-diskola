package id.diskola.app.di.module

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.diskola.app.dataclass.ResponData.AssignmentSchedule
import java.lang.reflect.Type
import java.util.Collections

@Module
@InstallIn(SingletonComponent::class)
object MoshiModule {

    @Reusable
    @Provides
    @JvmStatic
    fun instance(): Moshi {
        return Moshi.Builder()
//            .add(object : JsonAdapter.Factory {
//                override fun create(
//                    type: Type,
//                    annotations: MutableSet<out Annotation>,
//                    moshi: Moshi
//                ): JsonAdapter<*>? =
//                    try {
//                        moshi.nextAdapter<Any>(this, type, annotations).lenient()
//                    } catch (_: Exception) {
//                        moshi.nextAdapter<Any>(this, type, annotations).lenient()
//                    }
//            })
//
            // add adapter to parse single object to list
            .add(object : JsonAdapter.Factory {
                override fun create(
                    type: Type,
                    annotations: Set<Annotation>,
                    moshi: Moshi
                ): JsonAdapter<Any>? {
                    val delegateAnnotations =
                        Types.nextAnnotations(annotations, ObjectToList::class.java)
                            ?: return null
                    if (Types.getRawType(type) != List::class.java) throw IllegalArgumentException("Only lists may be annotated with @SingleToArray. Found: $type")
                    val elementType = Types.collectionElementType(type, List::class.java)
                    val delegateAdapter: JsonAdapter<List<Any>> =
                        moshi.adapter(type, delegateAnnotations)
                    val elementAdapter: JsonAdapter<Any> = moshi.adapter(elementType)

                    return ObjectToListAdapter(delegateAdapter, elementAdapter)
                }
            })

            // add adapter to null string
            .add(object {
                @ToJson
                fun toJson(@NullToEmptyString value: String?): String? = value

                @FromJson
                @NullToEmptyString
                fun fromJson(reader: JsonReader): String? {
                    val result = if (reader.peek() === JsonReader.Token.NULL) {
                        reader.nextNull()
                    } else {
                        reader.nextString()
                    }

                    return result ?: ""
                }
            })

            // add adapter to null int: a number the backend left out of a row (e.g. `"id": null` on a submission whose ids were invalid) reads as 0
            .add(object {
                @ToJson
                fun toJson(@NullToZeroInt value: Int): Int = value

                @FromJson
                @NullToZeroInt
                fun fromJson(reader: JsonReader): Int =
                    if (reader.peek() === JsonReader.Token.NULL) {
                        reader.nextNull<Any>()
                        0
                    } else {
                        reader.nextInt()
                    }
            })

            // A task made for a whole class (free mode) has no schedule: the backend sends `null` — or an empty array, the way PHP encodes an
            // empty map — instead of the schedule object. Either reads as "no schedule" instead of failing the whole page of tasks.
            .add(object : JsonAdapter.Factory {
                override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
                    if (annotations.isNotEmpty() || Types.getRawType(type) != AssignmentSchedule::class.java) return null
                    val delegate = moshi.nextAdapter<AssignmentSchedule>(this, type, annotations)
                    return object : JsonAdapter<AssignmentSchedule>() {
                        override fun fromJson(reader: JsonReader): AssignmentSchedule? = when (reader.peek()) {
                            JsonReader.Token.NULL -> reader.nextNull()
                            JsonReader.Token.BEGIN_ARRAY -> {
                                reader.skipValue()
                                null
                            }
                            else -> delegate.fromJson(reader)
                        }

                        override fun toJson(writer: JsonWriter, value: AssignmentSchedule?) = delegate.toJson(writer, value)
                    }
                }
            })

//            .add(object {
//                @ToJson
//                fun toJson(@JsonStringToInt value: Int?): Int? = value
//
//                @FromJson
//                @JsonStringToInt
//                fun fromJson(value: String?): Int = value?.toInt() ?: 0
//            })
            .build()
    }

    internal class ObjectToListAdapter(
        private val delegateAdapter: JsonAdapter<List<Any>>,
        private val elementAdapter: JsonAdapter<Any>
    ) : JsonAdapter<Any>() {

        override fun fromJson(reader: JsonReader): Any? =
            if (reader.peek() != JsonReader.Token.BEGIN_ARRAY)
                Collections.singletonList(elementAdapter.fromJson(reader))
            else
                delegateAdapter.fromJson(reader)

        // Writes the list itself (an empty body produced invalid JSON: the exam cache serialises `exams`).
        @Suppress("UNCHECKED_CAST")
        override fun toJson(writer: JsonWriter, value: Any?) {
            delegateAdapter.toJson(writer, value as List<Any>?)
        }
    }
}

@JsonQualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class NullToEmptyString

//@JsonQualifier
//@Retention(AnnotationRetention.RUNTIME)
//annotation class JsonStringToInt

/** `null` reads as `0` for an `Int` (see the adapter in [MoshiModule]). */
@JsonQualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class NullToZeroInt

@JsonQualifier
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
internal annotation class ObjectToList
