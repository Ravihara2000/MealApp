package com.example.meal_mania


import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class SearchByIngredientActivity : AppCompatActivity() {
    private lateinit var mealsTextView: TextView
    private lateinit var ingredientEditText: TextView
    private lateinit var retrieveMealsButton: Button
    private lateinit var saveMealsButton : Button

    // Initialize variables for database and DAO
    private lateinit var mealDao: MealDao
    private lateinit var mealDatabase: MealDatabase

    // Declare variable to hold retrieved meals
    private var meals: List<Meal>? = null

    // Set up the UI
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_by_ingredient)
        supportActionBar?.hide()

        mealsTextView = findViewById(R.id.meal_details)
        ingredientEditText = findViewById(R.id.ingredientEditText)
        retrieveMealsButton = findViewById(R.id.retrieve_meals_button)
        saveMealsButton = findViewById(R.id.save_meals_button)

        // Initialize the database and dao
        mealDatabase = MealDatabase.getInstance(applicationContext)
        mealDao = mealDatabase.mealDao()


        // Get ingredient from intent
        val ingredient = intent.getStringExtra("ingredient")

        // Launch a coroutine on IO dispatcher
        CoroutineScope(Dispatchers.IO).launch {
            // Retrieve meals from the web service
            val meals = ingredient?.let { retrieveMealsFromWebService(it) }

            // Update UI with search results on the main thread
            withContext(Dispatchers.Main) {
            }
        }

        // Add click listener to Retrieve Meals button
        retrieveMealsButton.setOnClickListener {
            val searchIngredient = ingredientEditText.text.toString()
            CoroutineScope(Dispatchers.IO).launch {
                val meals = retrieveMealsFromWebService(searchIngredient)
                withContext(Dispatchers.Main) {
                    if (meals.isEmpty()) {
                        // Display an error message if there are no meals for the given ingredient
                        Toast.makeText(
                            this@SearchByIngredientActivity,
                            "No meals found for $searchIngredient",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val builder = AlertDialog.Builder(this@SearchByIngredientActivity)
                        builder.setTitle("Meal Details")

                        // Set the message of the dialog box to be the meal details
                        builder.setMessage(meals.joinToString(separator = "\n\n") { mealDetailsString(it) })

                        // Add a button to close the dialog box
                        builder.setPositiveButton("OK", null)

                        // Show the dialog box
                        val dialog = builder.create()
                        dialog.show()

                        meals?.let {
                            this@SearchByIngredientActivity.meals=it
                        }
                    }
                }
            }
        }
        saveMealsButton.setOnClickListener {
            // Launch a coroutine on IO dispatcher
            CoroutineScope(Dispatchers.IO).launch {
                meals?.let {
                    mealDao.insertAll(it)
                    // Update UI with a toast on the main thread
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SearchByIngredientActivity, "Meals saved to database", Toast.LENGTH_SHORT).show()
                    }
                } ?: run {
                    // Update UI with a toast on the main thread if meals is null
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SearchByIngredientActivity, "No meals to save", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    //Saving the instances when the application rotating
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save any necessary data here
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle orientation change here if needed
    }

    // Define a private suspend function to retrieve meals from a web service
    private suspend fun retrieveMealsFromWebService(ingredient: String): List<Meal> {
        // Make HTTP request to the web service
        val url = "https://www.themealdb.com/api/json/v1/1/filter.php?i=$ingredient"
        val response = URL(url).readText()

        // Parse JSON response
        val mealsJson = JSONObject(response).optJSONArray("meals")
        if (mealsJson == null) {
            // Handle error here, e.g. throw an exception or return an empty list
            return emptyList()
        }

        // Create a list of meals from the retrieved JSON data
        val mealList = mutableListOf<Meal>()
        for (i in 0 until mealsJson.length()) {
            val mealId = mealsJson.getJSONObject(i).getString("idMeal")
            val mealDetailsResponse =
                URL("https://www.themealdb.com/api/json/v1/1/lookup.php?i=$mealId").readText()
            val mealDetailsJson = JSONObject(mealDetailsResponse).optJSONArray("meals")
            if (mealDetailsJson == null || mealDetailsJson.length() == 0) {
                // Handle error here, e.g. log a message or skip this meal
                continue
            }

            // Retrieve details for each meal
            val meal = mealDetailsJson.getJSONObject(0)
            val id = meal.getString("idMeal").toInt()
            val mealName = meal.getString("strMeal")
            val category = meal.getString("strCategory")
            val area = meal.getString("strArea")
            val instructions = meal.getString("strInstructions")
            val tags = meal.optString("strTags", "")
            val youtube = meal.optString("strYoutube", "")
            val mealThumb = meal.optString("strMealThumb", "")
            val ingredients = mutableListOf<String>()
            val measures = mutableListOf<String>()
            for (j in 1..20) {
                val ingredient = meal.optString("strIngredient$j", "")
                if (ingredient.isNotEmpty()) {
                    ingredients.add(ingredient)
                    measures.add(meal.optString("strMeasure$j", ""))
                }
            }
            val mealDetail = Meal(
                id=id,
                name = mealName,
                category = category,
                area = area,
                instructions = instructions,
                tags = tags,
                youtubeLink = youtube,
                ingredients = ingredients,
                measures = measures,
                mealThumb = mealThumb
            )
            mealList.add(mealDetail)
        }
        return mealList
    }


    // Define a function to return a string representation of meal details
    fun mealDetailsString(meal: Meal): String {
        val sb = StringBuilder()
        sb.append("Meal: ${meal.name}\n")
        sb.append("Category: ${meal.category}\n")
        sb.append("Area: ${meal.area}\n")
        sb.append("Instructions: ${meal.instructions}\n")
        sb.append("Tags: ${meal.tags}\n")
        sb.append("Youtube: ${meal.youtubeLink}\n")
        sb.append("Thumbnail URL: ${meal.mealThumb}\n")

        sb.append("Ingredients:\n")
        for (i in meal.ingredients.indices) {
            sb.append("Ingredient${i + 1}: ${meal.ingredients[i]}\n")
        }
        sb.append("\nMeasures:\n")
        for (i in meal.measures.indices) {
            sb.append("Measure${i + 1}: ${meal.measures[i]}\n")
        }

        return sb.toString()
    }
}





