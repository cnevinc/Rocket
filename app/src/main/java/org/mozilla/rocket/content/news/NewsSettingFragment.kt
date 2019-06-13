package org.mozilla.rocket.content.news

import android.arch.lifecycle.Observer
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ProgressBar
import dagger.android.support.AndroidSupportInjection
import org.mozilla.focus.R
import org.mozilla.rocket.content.news.data.CategorySetting
import org.mozilla.rocket.content.news.data.NewsCatSettingCatAdapter
import org.mozilla.rocket.content.news.data.NewsLanguagePreference
import org.mozilla.rocket.content.news.data.NewsSettingsLocalDataSource
import org.mozilla.rocket.content.news.data.NewsSettingsRemoteDataSource
import org.mozilla.rocket.content.news.data.NewsSettingsRepository
import org.mozilla.threadutils.ThreadUtils
import java.lang.Thread.sleep

class NewsSettingFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    @javax.inject.Inject
    lateinit var applicationContext: Context

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AndroidSupportInjection.inject(this)
        val remoteSource = NewsSettingsRemoteDataSource()
        val localSource = NewsSettingsLocalDataSource(applicationContext)
        val repository = NewsSettingsRepository(remoteSource, localSource)

        // FIXME: hard code
        val newsLanguagePreference = findPreference("user_pref_lang") as NewsLanguagePreference

        repository.getLanguages().observe(viewLifecycleOwner, Observer {
            newsLanguagePreference.updateLangList(it)
        })
        repository.getUserPreferenceLanguage().observe(viewLifecycleOwner, Observer {
            newsLanguagePreference.summary = it?.name
        })
    }

    companion object {
        fun newInstance(): NewsSettingFragment {
            return NewsSettingFragment()
        }
    }

    override fun onCreatePreferences(p0: Bundle?, p1: String?) {
        addPreferencesFromResource(R.xml.settings_news)
    }

    override fun onResume() {
        super.onResume()

        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()

        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // detect when the user had change the language setting, now refresh the categories
        if (key == "user_pref_lang") {

            // find hte view on activity... it's not pretty,
            // TODO: we should let the update the cats via repository/use case, and let [NewsCategoryPreference] change it's data
            val list = (context as? FragmentActivity)?.findViewById<RecyclerView>(R.id.news_setting_cat_list)
            val progress = (context as? FragmentActivity)?.findViewById<ProgressBar>(R.id.news_setting_cat_progress)

            list?.visibility = View.GONE
            progress?.visibility = View.VISIBLE

            (list?.adapter as? NewsCatSettingCatAdapter)?.apply {
                ThreadUtils.postToBackgroundThread {
                    sleep(3000)
                    ThreadUtils.postToMainThread {
                        this.cats =
                            listOf(
                                CategorySetting("D", "DD"),
                                CategorySetting("E", "EE"),
                                CategorySetting("F", "FF")
                            )
                        this.notifyDataSetChanged()
                        list.visibility = View.VISIBLE
                        progress?.visibility = View.GONE
                    }
                }
            }
        }
    }
}