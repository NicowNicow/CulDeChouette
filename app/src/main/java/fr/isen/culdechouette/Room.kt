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
    var ready_boolean: Boolean) {

    constructor() : this(
        "empty_key",
        "empty_username",
        0,
        false)
}


class GameParameters (
    var user_turn: String,
    var dice_values: DiceValues) {

    constructor(): this(
        "empty_user_turn",
        DiceValues())
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
