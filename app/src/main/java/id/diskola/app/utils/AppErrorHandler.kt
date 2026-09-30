package id.diskola.app.utils

import id.diskola.app.apiservice.ApiException
import java.io.IOException
import java.net.SocketTimeoutException

object AppErrorHandler {

    fun getMessage(throwable: Throwable): String {
        return when (throwable) {
            is ApiException -> mapApiException(throwable)
            is SocketTimeoutException -> "Koneksi ke server terlalu lama. Silakan coba lagi."
            is IOException -> "Terjadi gangguan pada koneksi internet Anda. Silakan coba lagi."
            else -> throwable.message ?: "Terjadi kesalahan pada aplikasi."
        }
    }

    private fun mapApiException(exception: ApiException): String {
        val serverMessage = exception.message?.takeIf { it.isNotBlank() && it != "null" }

        return when (exception.responseCode) {
            400 -> serverMessage ?: "Permintaan tidak valid."
            401 -> "Sesi Anda sudah habis. Silakan login kembali."
            403 -> serverMessage ?: "Anda tidak memiliki akses ke fitur ini."
            404 -> serverMessage ?: "Data tidak ditemukan."
            409 -> serverMessage ?: "Data yang sama sudah ada."
            422 -> serverMessage ?: "Data yang dikirim belum valid."
            429 -> serverMessage ?: "Terlalu banyak permintaan. Silakan tunggu sebentar."
            500 -> serverMessage ?: "Terjadi gangguan pada server."
            502 -> serverMessage ?: "Server perantara sedang bermasalah."
            503 -> serverMessage ?: "Layanan sedang sibuk atau maintenance."
            504 -> serverMessage ?: "Server terlalu lama merespons."
            else -> serverMessage ?: "Terjadi kesalahan. Kode: ${exception.responseCode ?: "-"}"
        }
    }
}