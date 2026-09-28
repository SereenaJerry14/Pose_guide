package com.photopose.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.photopose.app.data.model.PoseCatalog
import com.photopose.app.data.model.PoseCategory
import com.photopose.app.data.model.TargetPoseTemplate
import java.io.InputStreamReader

class PoseRepository(private val context: Context) {

    private val gson = Gson()
    private var cachedCatalog: PoseCatalog? = null

    fun loadCatalog(): PoseCatalog {
        cachedCatalog?.let { return it }

        return try {
            context.assets.open("pose_library.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val catalog = gson.fromJson(reader, PoseCatalog::class.java)
                    cachedCatalog = catalog
                    catalog
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            PoseCatalog(emptyList(), emptyList())
        }
    }

    fun getCategories(): List<PoseCategory> {
        return loadCatalog().categories
    }

    fun getPosesByCategory(categoryId: String): List<TargetPoseTemplate> {
        val allPoses = loadCatalog().poses
        return if (categoryId.isEmpty() || categoryId == "all") {
            allPoses
        } else {
            allPoses.filter { it.categoryId == categoryId }
        }
    }

    fun getPoseById(poseId: String): TargetPoseTemplate? {
        return loadCatalog().poses.find { it.id == poseId }
    }
}
