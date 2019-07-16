package com.poc.eventbusrx

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import rx.schedulers.Schedulers
import rx.subjects.UnicastSubject
import kotlin.math.roundToLong


class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = MainActivity::class.java.simpleName
    }

    val eventQueue: UnicastSubject<MessageEvent> = UnicastSubject.create()

    val processor = Processor()

    private val eventBus = EventBus.getDefault()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startBtn.setOnClickListener {
            val events = 100
            sendEventsToQueue("TH1", events)
//            sendEventsToQueue("TH2", events)
//            sendEventsToQueue("TH3", events)
        }

        breakBtn.setOnClickListener {
            val events = 100
            sendEventsToWild("TH1", events)
            sendEventsToWild("TH2", events)
            sendEventsToWild("TH3", events)
        }

        observeQueue()

    }

    private fun sendEventsToQueue(threadName: String, events: Int) {
        Thread {
            run {
                val name = Thread.currentThread().name
                for (i in 1..events) {
                    //Sending events sequentially
                    eventBus.post(MessageEvent("$i $name"))
                }
                Log.i(TAG, "All messages sent to queue from $name")
            }
        }
                .apply { name = threadName }
                .start()
    }

    private fun sendEventsToWild(threadName: String, events: Int) {
        Thread {
            run {
                val name = Thread.currentThread().name
                for (i in 1..events) {
                    processor.process(MessageEvent("$i $name"))
                }
                Log.i(TAG, "All messages sent to queue from $name")
            }
        }
                .apply { name = threadName }
                .start()
    }

    private fun observeQueue() {
        eventQueue
                .subscribeOn(Schedulers.io())
                .subscribe {
                    processor.process(it)
                }
    }

    override fun onPause() {
        super.onPause()
        eventBus.unregister(this)
    }

    override fun onResume() {
        super.onResume()
        eventBus.register(this)
    }


    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEvent(event: MessageEvent) {
        Log.i(TAG, "Event $event sent to queue")
        eventQueue.onNext(event)
    }

    data class MessageEvent(val name: String)

    class Processor {

        var currentValue: String? = null

        fun process(event: MessageEvent) {
            Log.i(TAG, "Received $event.name")
            currentValue = event.name
            Thread.sleep((1000 * Math.random()).roundToLong())
            if (currentValue != event.name) {
                throw Error("${currentValue} != ${event.name}")
            }
        }

    }

}
