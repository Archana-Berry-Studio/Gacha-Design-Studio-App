package com.lunime.githubcollab.archanaberry.gachadesignstudio.gachadynamicstorage

import android.os.Process

object GachaDynamicStorage {

    // Fungsi untuk mendeteksi direktori dinamis
    fun detectDynamicDirectory(): String {
        // Mendapatkan ID pengguna yang unik
        val userId = Process.myUserHandle().hashCode()

        // Mengembalikan path penyimpanan sesuai dengan ID pengguna
        return if (userId == 0) {
            "/storage/emulated/0" // Penyimpanan untuk pengguna pertama (user 0)
        } else {
            "/storage/emulated/$userId" // Penyimpanan untuk pengguna lainnya
        }
    }
}