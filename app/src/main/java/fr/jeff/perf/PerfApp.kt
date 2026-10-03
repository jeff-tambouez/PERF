package fr.jeff.perf

import android.app.Application
import fr.jeff.perf.data.PerfDatabase
import fr.jeff.perf.data.PerfRepository

class PerfApp : Application() {
    val repository: PerfRepository by lazy { PerfRepository(PerfDatabase.creer(this)) }
}
