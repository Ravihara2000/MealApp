package com.example.mealappnew

import android.app.Dialog
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.mealapp2.R
import com.example.mealapp2.classes.AppDatabase
import com.example.mealapp2.classes.MealDao
import com.example.mealapp2.classes.Meals

class Search_By_Ingredient :AppCompatActivity(){
    var allMeals  = arrayListOf<Meals>();
    lateinit var cardScroll: LinearLayout

    lateinit var linearLayout : LinearLayout
    lateinit var mealDao : MealDao

    var mydialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_by_ingredient)

        val db = Room.databaseBuilder(this, AppDatabase::class.java,
            "mealdatabase").build()
        mealDao = db.mealDao()

        mydialog=Dialog(this)

    }
}