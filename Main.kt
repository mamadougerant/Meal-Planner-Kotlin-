package mealplanner

import java.io.File
import java.sql.DriverManager
import java.sql.Statement
import java.util.*

data class Meal(val category: String, val name: String, val ingredients: MutableSet<String>)

val CATEGORIES = arrayOf("breakfast", "lunch", "dinner")
val regexChars = Regex("^[A-Za-z, ]+$")

fun main() {
    val connection = DriverManager.getConnection("jdbc:sqlite:meals.db")
    val statement: Statement = connection.createStatement()

    statement.executeUpdate("create table if not exists meals (meal_id integer primary key autoincrement, category text, meal text)")
    statement.executeUpdate("create table if not exists ingredients (ingredient_id integer primary key, ingredient text, meal_id integer, foreign key (meal_id) references meals(meal_id))")
    //statement.executeUpdate("drop table if exists plan ")
    statement.executeUpdate("create table if not exists plan (meal_id integer primary key autoincrement, category text, opt text,foreign key (meal_id) references meals(meal_id))")


    fun checkInput(input: String): Boolean {
        return input.isNotEmpty() && regexChars.matches(input) && input.count { it == ',' } == input.split(",").filter { it.trim().isNotBlank() }.toSet().size - 1
    }

    fun getInputFromOptions(message: String, options: Array<String>): String {
        while (true) {
            val input = readln()
            if (options.contains(input))
                return input
            println("$message ${options.joinToString(", ")}.")
        }
    }

    fun getInputWithValidation(errorMessage: String): String {
        while (true) {
            val input = readln()
            if (checkInput(input))
                return input
            println(errorMessage)
        }
    }

    fun inputMeal(): Meal {
        println("Which meal do you want to add (breakfast, lunch, dinner)?")
        val category = getInputFromOptions("Wrong meal category! Choose from:", CATEGORIES)

        println("Input the meal's name:")
        val name = getInputWithValidation("Wrong format. Use letters only!")

        println("Input the ingredients:")
        val ingredients = getInputWithValidation("Wrong format. Use letters only!")
            .split(",").map { it.trim() }.toMutableSet()

        return Meal(category, name, ingredients)
    }

    fun showMeals() {
        println("Which category do you want to print (breakfast, lunch, dinner)?")
        val category = getInputFromOptions("Wrong meal category! Choose from: breakfast, lunch, dinner.", CATEGORIES)
        val query = """
        SELECT meals.meal_id, meals.category, meals.meal, GROUP_CONCAT(ingredients.ingredient, ', ') AS ingredient_list
        FROM meals
        LEFT JOIN ingredients ON meals.meal_id = ingredients.meal_id
        WHERE meals.category = '$category'
        GROUP BY meals.meal_id
    """.trimIndent()

        val rs = statement.executeQuery(query)
        val response = mutableListOf<String>()

        while (rs.next()) {
            //val categoryOfMeal = rs.getString("category")
            val name = rs.getString("meal")
            val ingredientList = rs.getString("ingredient_list")
            response += ("Name: $name")
            if (ingredientList != null) {
                response += ("Ingredients:\n${ingredientList.split(",").joinToString("\n")}")
            }
        }
        if(response.isNotEmpty()) {
            println("Category: $category")
            println(response.joinToString("\n"))
        }else println("No meals found.")

    }
    fun plan (){
        val days = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")
        days.forEach {day->
            println(day)
            listOf("breakfast","lunch","dinner").forEach {opt->
                val query = """
                SELECT meals.meal_id, meals.category, meals.meal, GROUP_CONCAT(ingredients.ingredient, ', ') AS ingredient_list
                FROM meals
                LEFT JOIN ingredients ON meals.meal_id = ingredients.meal_id
                WHERE meals.category = '$opt'
                GROUP BY meals.meal_id
            """.trimIndent()
                val rs = statement.executeQuery(query)
                val result = mutableListOf<String>()
                while (rs.next()) {
                    val name = rs.getString("meal")
                    result += (name)
                }
                println(result.sorted().joinToString("\n"))
                println("Choose the $opt for $day from the list above:")
                while (true) {
                    val choice = readln()
                    if (!result.contains(choice)) println("This meal doesn’t exist. Choose a meal from the list above.")
                    else {
                        val mealsInsertQuery = "insert into plan (category, opt) values('$opt', '$choice')"
                        statement.executeUpdate(mealsInsertQuery)
                        break
                    }
                }
            }
            println("Yeah! We planned the meals for $day.")
        }

        val query = "SELECT * FROM plan"
        val rs = statement.executeQuery(query)
        val result = mutableListOf<String>()
        while (rs.next()) {
            val category = rs.getString("category")
            val option = rs.getString("opt")
            result += ("$category: $option")
        }
        val resultSet = result.chunked(3)
        for (i in resultSet.indices){
            val progOftheDay = resultSet[i]
            println(days[i])
            println(progOftheDay.joinToString("\n"))
        }
    }

    fun save(){
        val query = "SELECT * FROM plan"
        val rs = statement.executeQuery(query)
        val result = mutableListOf<String>()
        while (rs.next()) {
            val category = rs.getString("category")
            val option = rs.getString("opt")
            result += ("$category: $option")
        }
        if (result.isEmpty()) {
            println("Unable to save. Plan your meals first.")
            return
        }
        println("Input a filename:")
        val file = File (readln())
        file.createNewFile()
        println("Saved!")
    }



    var input: String
    do {
        println("What would you like to do (add, show, plan, save, exit)?")
        input = readln()
        when (input) {
            "add" -> {
                val meal = inputMeal()
                println("The meal has been added!")

                val category = meal.category
                val name = meal.name
                val ing = meal.ingredients.joinToString(",")

                val mealsInsertQuery = "insert into meals (category, meal) values('$category', '$name')"
                statement.executeUpdate(mealsInsertQuery)

                val generatedKeys = statement.executeQuery("SELECT last_insert_rowid()")
                val mealId = generatedKeys.getInt(1)

                val ingInsertQuery = "insert into ingredients (ingredient, meal_id) values('$ing', $mealId)"
                statement.executeUpdate(ingInsertQuery)
            }

            "show" -> {
                showMeals()
            }
            "plan"->{
                plan()
            }
            "save"->{
                save()
            }
            "exit" -> println("Bye!")
        }
    } while (input != "exit")

    statement.close()
    connection.close()
}
