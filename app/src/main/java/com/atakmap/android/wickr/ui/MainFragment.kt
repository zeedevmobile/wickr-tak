package com.atakmap.android.wickr.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.atakmap.android.maps.MapView
import com.atakmap.android.wickr.Requests
import com.atakmap.android.wickr.WickrMapComponent
import com.atakmap.android.wickr.plugin.R
import com.atakmap.android.wickr.utils.SettingsManager
import com.wickr.android.api.WickrAPIObjects
import kotlinx.android.synthetic.main.main_layout.tabPager
import kotlinx.android.synthetic.main.main_layout.tabTitle
import kotlinx.android.synthetic.main.main_layout.tab_left_dot
import kotlinx.android.synthetic.main.main_layout.tab_middle_dot
import kotlinx.android.synthetic.main.main_layout.tab_right_dot
import kotlinx.android.synthetic.main.main_layout.tab_wear_dot
import org.greenrobot.eventbus.Subscribe

class TabCollectionPagerAdpater(private val fm: FragmentManager, private val pluginContext: Context, private val fragments: ArrayList<Fragment>) : FragmentStatePagerAdapter(fm) {
    override fun getCount(): Int {
        return 4
    }

    override fun getItem(pos: Int): Fragment {
        if(fragments[pos].isAdded){
            if (fragments[pos] is ContactsListFragment) {
                fm.beginTransaction().detach(fragments[pos]).attach(fragments[pos]).commit()
            } else {
                fm.beginTransaction().remove(fragments[pos]).commit()
            }
        }
        return fragments[pos]
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> pluginContext.getString(R.string.home_tab_rooms)
            1 -> pluginContext.getString(R.string.home_tab_dms)
            2 -> pluginContext.getString(R.string.home_tab_contacts)
            3 -> "Wear sample"
            else -> "Unknown"
        }
    }

}


class MainFragment(private val pluginContext: Context, private val mapView: MapView) : Fragment(), ViewPager.OnPageChangeListener {

    private var settingsManager: SettingsManager = SettingsManager(pluginContext)

    private lateinit var viewPager: ViewPager
    private lateinit var tabCollectionPagerAdpater: TabCollectionPagerAdpater

    private var tabDots: ArrayList<View> = arrayListOf()

    private var selectedPos = 0


    companion object {
        val TAG = MainFragment.javaClass.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(pluginContext).inflate(R.layout.main_layout, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabDots.add(tab_left_dot)
        tabDots.add(tab_middle_dot)
        tabDots.add(tab_right_dot)
        tabDots.add(tab_wear_dot)

        val fragments: MutableList<Fragment> = java.util.ArrayList()
        fragments.add(
            RoomsListFragment.newInstance(pluginContext, WickrAPIObjects.WickrConvo.ConvoType.ROOM)!!
        )
        fragments.add(
            RoomsListFragment.newInstance(pluginContext, WickrAPIObjects.WickrConvo.ConvoType.DM)!!
        )
        fragments.add(
            ContactsListFragment.newInstance(pluginContext)!!
        )
        fragments.add(
            WearSampleFragment.newInstance(pluginContext)!!
        )
        tabCollectionPagerAdpater = TabCollectionPagerAdpater(
            childFragmentManager, pluginContext, fragments as ArrayList<Fragment>
        )

        tabPager.adapter = tabCollectionPagerAdpater
        tabPager.adapter?.notifyDataSetChanged()
        tabPager.addOnPageChangeListener(this)
        tabPager.isSaveEnabled = false
        selectedPos = settingsManager.getMainFragPos()
        tabDots[selectedPos].isSelected = true
        updateDots()
    }

    fun refreshPager() { updateDots() }

    override fun onStart() {
        super.onStart()
        WickrMapComponent.EVENTBUS.register(this)
        updateDots()
        // request user settings if app is paired
        if (settingsManager.isPaired()) {
            val requests = Requests(pluginContext)
            requests.settings()
        }
    }



    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        updateDots()
    }

    private fun updateDots() {
        Log.d(TAG, "Updating the dots")
        tabDots[selectedPos].isSelected = true
        //tabDots[selectedPos].background = pluginContext.getDrawable(R.drawable.dot_selected)
        if (tabPager != null) {
            tabPager.currentItem = selectedPos
            tabPager.refreshDrawableState()
        }
    }

    override fun onStop() {
        super.onStop()
        settingsManager.saveMainFragPos(selectedPos)
        WickrMapComponent.EVENTBUS.unregister(this)
    }

    override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
        updateDots()
    }

    override fun onPageSelected(pos: Int) {
        for (i in tabDots.indices) {
            tabDots[i].isSelected = pos == i
        }
        when (pos) {
            0 -> tabTitle.text = pluginContext.getString(R.string.home_tab_rooms)
            1 -> tabTitle.text = pluginContext.getString(R.string.home_tab_dms)
            2 -> tabTitle.text = pluginContext.getString(R.string.home_tab_contacts)
            3 -> tabTitle.text = "Wear sample"
        }
        selectedPos = pos
        updateDots()
        if (pos == 2) {
            tabCollectionPagerAdpater.getItem(2)
        }
    }

    override fun onPageScrollStateChanged(p0: Int) {
        updateDots()
    }

    @Subscribe
    internal fun onWickrAPIEvent(event: Any) {
        /*when (event) {
            //is WickrAPIPairedEvent -> invalidateOptionsMenu()
            //is WickrAPIUnpairedEvent -> invalidateOptionsMenu()
            is WickrUserSettingsEvent -> {
                UserSettings.selfUser = event.userSettings.selfUser
                UserSettings.maxRoomMembers = event.userSettings.maxRoomMembers
                UserSettings.canStartCalls = event.userSettings.canStartCalls
            }
        }*/
    }
}