package at.gdev.contacts.data.network

import at.gdev.contacts.data.network.dto.ReferenceListResponse
import retrofit2.http.GET

interface ReferenceApi {
    @GET("reference/genders")
    suspend fun genders(): ReferenceListResponse

    @GET("reference/countries")
    suspend fun countries(): ReferenceListResponse

    @GET("reference/contact-groups")
    suspend fun contactGroups(): ReferenceListResponse
}
