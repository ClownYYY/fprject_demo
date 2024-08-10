package com.deniscerri.ytdl.ui.more.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.deniscerri.ytdl.BuildConfig
import com.deniscerri.ytdl.R
import com.deniscerri.ytdl.util.UiUtil
import com.deniscerri.ytdl.util.UpdateUtil
import com.google.android.material.snackbar.Snackbar
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class UpdateSettingsFragment : BaseSettingsFragment() {
    override val title: Int = R.string.updating
    private var updateYTDL: Preference? = null
    private var ytdlVersion: Preference? = null
    private var ytdlSource: Preference? = null
    private var updateUtil: UpdateUtil? = null
    private var version: Preference? = null



    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.updating_preferences, rootKey)
        updateUtil = UpdateUtil(requireContext())
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        val editor = preferences.edit()
        updateYTDL = findPreference("update_ytdl")
        ytdlVersion = findPreference("ytdl-version")
        ytdlSource = findPreference("ytdlp_source")

        YoutubeDL.getInstance().version(context)?.let {
            editor.putString("ytdl-version", it)
            editor.apply()
            ytdlVersion!!.summary = it
        }

        ytdlSource?.setOnPreferenceChangeListener { preference, newValue ->
            initYTDLUpdate(editor)
            true
        }

        updateYTDL!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                initYTDLUpdate(editor)
                true
            }


        findPreference<Preference>("changelog")?.setOnPreferenceClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                updateUtil?.showChangeLog(requireActivity())
            }
            true
        }


        version = findPreference("version")
        val nativeLibraryDir = context?.applicationInfo?.nativeLibraryDir
        version!!.summary = "${BuildConfig.VERSION_NAME} [${nativeLibraryDir?.split("/lib/")?.get(1)}]"
        version!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                lifecycleScope.launch{
                    withContext(Dispatchers.IO){
                        updateUtil!!.updateApp{ msg ->
                            lifecycleScope.launch(Dispatchers.Main){
                                view?.apply {
                                    Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show()
                                }
                            }
                        }
                    }

                }
                true
            }

    }

    private fun initYTDLUpdate(editor: SharedPreferences.Editor) = lifecycleScope.launch {
        Snackbar.make(requireView(),
            requireContext().getString(R.string.ytdl_updating_started),
            Snackbar.LENGTH_LONG).show()
        runCatching {
            when (updateUtil!!.updateYoutubeDL()) {
                YoutubeDL.UpdateStatus.DONE -> {
                    Snackbar.make(requireView(),
                        requireContext().getString(R.string.ytld_update_success),
                        Snackbar.LENGTH_LONG).show()

                    YoutubeDL.getInstance().version(context)?.let {
                        editor.putString("ytdl-version", it)
                        editor.apply()
                        ytdlVersion!!.summary = it
                    }
                }
                YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> Snackbar.make(requireView(),
                    requireContext().getString(R.string.you_are_in_latest_version),
                    Snackbar.LENGTH_LONG).show()
                else -> {
                    Snackbar.make(requireView(),
                        requireContext().getString(R.string.errored),
                        Snackbar.LENGTH_LONG).show()
                }
            }
        }.onFailure {
            val msg = it.message ?: requireContext().getString(R.string.errored)
            view?.apply {
                val snackBar = Snackbar.make(this, msg, Snackbar.LENGTH_LONG)
                snackBar.setAction(R.string.copy_log){
                    UiUtil.copyToClipboard(msg, requireActivity())
                }
                val snackbarView: View = snackBar.view
                val snackTextView = snackbarView.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
                snackTextView.maxLines = 9999999
                snackBar.show()
            }

        }
    }


}