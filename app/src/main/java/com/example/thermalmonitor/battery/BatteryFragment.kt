package com.example.thermalmonitor.battery



import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.thermalmonitor.databinding.FragmentBatteryBinding

class BatteryFragment : Fragment() {

    private var _binding: FragmentBatteryBinding? = null // view binding for this fragment
    private val binding get() = _binding!!

    private val viewModel: BatteryViewModel by viewModels() // view model for this fragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBatteryBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // observe the live data from the view model and update the UI accordingly
        viewModel.batteryData.observe(viewLifecycleOwner, Observer { data ->
            binding.apply {
                batteryLevel.text = "${data.level}%"
                batteryStatus.text = data.status
                batteryCurrent.text = "${data.current} mA"
                batteryTemperature.text = "${data.temperature} °C"
                batteryVoltage.text = "${data.voltage} V"
                batterySource.text = data.source
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // register a broadcast receiver to listen for battery changes
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        requireActivity().registerReceiver(viewModel.batteryReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        // unregister the broadcast receiver when the fragment is paused
        requireActivity().unregisterReceiver(viewModel.batteryReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // avoid memory leaks
    }
}



