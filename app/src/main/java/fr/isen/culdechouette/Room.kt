package fr.isen.culdechouette

class Room (
    var room_key: String,
    var room_name: String,
    var user_count: Int,
    var capacity: Int,
    var room_password_needed: Boolean,
    var game_started_boolean: Boolean,
    var room_password: String,
    var game_parameters: GameParameters) {

    constructor() : this(
        "empty_key",
        "empty_name",
        0,
        2,
        false,
        false,
        "password",
        GameParameters())
}

class User (
    var user_key: String,
    var username: String,
    var score: Int,
    var ready_boolean: Boolean,
    var sum_pasmou: Int,
    var sum_grelotte: Int) {

    constructor() : this(
        "empty_key",
        "empty_username",
        0,
        false,
        0,
        0)
}


class GameParameters (
    var user_turn: String,
    var timer_launched : Int,
    var dice_values: DiceValues,
    var dice_value_changed: Boolean,
    var dice_figure: DiceFigure) {

    constructor(): this(
        "empty_user_turn",
        0,
        DiceValues(),
        false,
        DiceFigure())
}

class DiceFigure(
    var figure: String,
    var valueFigure: Int,
    var sirop_dice: Int) {

    constructor(): this(
        "neant",
        0,
        0)

}

class DiceValues (
    var dice_1: Int,
    var dice_2: Int,
    var dice_3: Int) {

    constructor(): this(
        1,
        1,
        1)
}
