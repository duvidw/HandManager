package com.davw.handcommandserver

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class DirectCommandConfig(
    val name: String,
    val action: String
)

fun parseDirectCommands(jsonText: String): List<DirectCommandConfig> {
    val root = JSONObject(jsonText)
    if (root.has("commands")) {
        val jsonCommands = root.getJSONArray("commands")
        val commands = mutableListOf<DirectCommandConfig>()
        for (index in 0 until jsonCommands.length()) {
            val commandObject = jsonCommands.getJSONObject(index)
            commands.add(
                DirectCommandConfig(
                    name = commandObject.getString("name"),
                    action = commandObject.getString("action")
                )
            )
        }
        return commands
    }

    throw JSONException("Missing commands array")
}

fun directCommandsToJson(commands: List<DirectCommandConfig>): String {
    val root = JSONObject()
    val jsonCommands = JSONArray()
    commands.forEach { command ->
        val item = JSONObject()
        item.put("name", command.name)
        item.put("action", command.action)
        jsonCommands.put(item)
    }
    root.put("commands", jsonCommands)
    return root.toString()
}
