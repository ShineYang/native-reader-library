package com.shawnyang.jpreader_lib.ui.reader.outline

import android.os.Bundle
import org.readium.r2.shared.publication.Locator

/**
 * @author ShineYang
 * @date 2021/9/6
 * description:
 */
object OutlineContract {

    private const val DESTINATION_KEY = "locator"

    val REQUEST_KEY: String = OutlineContract::class.java.name

    data class Result(val destination: Locator)

    fun createResult(locator: Locator): Bundle =
        Bundle().apply { putParcelable(DESTINATION_KEY, locator) }

    fun parseResult(result: Bundle): Result {
        val destination = requireNotNull(result.getParcelable<Locator>(DESTINATION_KEY))
        return Result(destination)
    }
}