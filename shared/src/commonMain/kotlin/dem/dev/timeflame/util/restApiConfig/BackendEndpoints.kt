package dem.dev.timeflame.util.restApiConfig

object BackendEndpoints {

    private const val API = "89.111.173.185"
    private const val PORT = "8080"
    private const val API_URL = "http://$API:$PORT/api/v1"

    private const val auth = "auth"
    private const val tasks = "tasks"
    private const val user = "user"

    object Auth {
        const val register = "$API_URL/$auth/register"
        const val login = "$API_URL/$auth/login"
    }

    object Task {
        const val getByUserIdInDiapason = "$API_URL/$tasks/getByUserIdInDiapason"
        const val create = "$API_URL/$tasks"
        const val delete = "$API_URL/$tasks/delete"
        const val edit = "$API_URL/$tasks/edit"
    }

    object User {
        const val getById = "$API_URL/$user/byId"
        const val resetPassword = "$API_URL/$user/resetPassword"
        val devicesTokens: (String) -> String = { userId -> "$API_URL/$user/$userId/devicesTokens"}
    }
}