package com.pablitosb.sportsbook.data.dfs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Canonical FanDuel salary import format used by DFS Lineups and FD DFS Projections.
 *
 * Required: name, team, pos, salary
 * Optional: proj, mlbId
 * Positions: P, C, 1B, 2B, 3B, SS, OF, DH
 */
object SampleSalaryCsv {
    const val FILE_NAME = "fanduel_salary_sample.csv"
    const val HEADER = "name,team,pos,salary,proj,mlbId"
    const val HINT =
        "Required: name, team, pos, salary. Optional: proj, mlbId. FanDuel pos: P, C, 1B, 2B, 3B, SS, OF, DH."

    /** Small shareable sample — 5 rows, including one with blank optional fields. */
    val CONTENTS = """
        $HEADER
        Tarik Skubal,DET,P,11200,33.8,669373
        Aaron Judge,NYY,OF,4500,18.8,592450
        Cal Raleigh,SEA,C,3300,,
        Freddie Freeman,LAD,1B,3900,13.6,518692
        Gunnar Henderson,BAL,SS,3400,12.0,683002
    """.trimIndent() + "\n"

    fun authority(context: Context): String = "${context.packageName}.fileprovider"

    /**
     * Writes the sample to cache and opens the system share sheet so the user can
     * save it in Files or open it in a spreadsheet. Falls back to clipboard copy.
     * @return user-facing status, or null if the share sheet launched.
     */
    fun share(context: Context): String? {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, FILE_NAME)
        file.writeText(CONTENTS)
        val uri = FileProvider.getUriForFile(context, authority(context), file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "FanDuel salary sample CSV")
            putExtra(Intent.EXTRA_TEXT, HINT)
            clipData = ClipData.newRawUri(FILE_NAME, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Share sample CSV")
        val launched = runCatching {
            context.startActivity(chooser)
        }.isSuccess
        return if (launched) {
            null
        } else {
            copyToClipboard(context)
            "Share sheet unavailable — sample CSV copied."
        }
    }

    fun copyToClipboard(context: Context) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(FILE_NAME, CONTENTS))
    }
}
