package com.example.mealapp2.classes

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Meals::class], version = 1)
@TypeConverters(StringListTypeConverter:: class)
abstract class AppDatabase : RoomDatabase(){
    abstract fun mealDao(): MealDao

}