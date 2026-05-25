package at.gdev.contacts.data.network

import at.gdev.contacts.data.network.dto.ContactAddressRequest
import at.gdev.contacts.data.network.dto.ContactByNumberResponse
import at.gdev.contacts.data.network.dto.ContactCallRequest
import at.gdev.contacts.data.network.dto.ContactCommentStoreRequest
import at.gdev.contacts.data.network.dto.ContactCommentUpdateRequest
import at.gdev.contacts.data.network.dto.ContactCommentsResponse
import at.gdev.contacts.data.network.dto.ContactDateRequest
import at.gdev.contacts.data.network.dto.ContactDetailResponse
import at.gdev.contacts.data.network.dto.ContactsStoreRequest
import at.gdev.contacts.data.network.dto.ContactEmailRequest
import at.gdev.contacts.data.network.dto.ContactGiftIdeaRequest
import at.gdev.contacts.data.network.dto.ContactNoteRequest
import at.gdev.contacts.data.network.dto.ContactNumberRequest
import at.gdev.contacts.data.network.dto.ContactUpdateRequest
import at.gdev.contacts.data.network.dto.ContactUrlRequest
import at.gdev.contacts.data.network.dto.ContactsListResponse
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ContactsApi {
    @GET("contacts")
    suspend fun list(
        @Query("q") query: String? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
    ): ContactsListResponse

    @POST("contacts")
    suspend fun create(@Body body: ContactsStoreRequest): ContactDetailResponse

    @GET("contacts/{contact}")
    suspend fun show(@Path("contact") ulid: String): ContactDetailResponse

    @GET("contacts/by-number")
    suspend fun byNumber(@Query("number") number: String): ContactByNumberResponse

    @PUT("contacts/{contact}")
    suspend fun update(@Path("contact") ulid: String, @Body body: ContactUpdateRequest): JsonElement

    @DELETE("contacts/{contact}")
    suspend fun delete(@Path("contact") ulid: String): Response<Unit>

    @POST("contacts/{contact}/numbers")
    suspend fun createNumber(@Path("contact") ulid: String, @Body body: ContactNumberRequest): JsonElement

    @PUT("contacts/{contact}/numbers/{number}")
    suspend fun updateNumber(
        @Path("contact") ulid: String,
        @Path("number") numberId: String,
        @Body body: ContactNumberRequest,
    ): JsonElement

    @DELETE("contacts/{contact}/numbers/{number}")
    suspend fun deleteNumber(
        @Path("contact") ulid: String,
        @Path("number") numberId: String,
    ): Response<Unit>

    @POST("contacts/{contact}/emails")
    suspend fun createEmail(@Path("contact") ulid: String, @Body body: ContactEmailRequest): JsonElement

    @PUT("contacts/{contact}/emails/{email}")
    suspend fun updateEmail(
        @Path("contact") ulid: String,
        @Path("email") emailId: String,
        @Body body: ContactEmailRequest,
    ): JsonElement

    @DELETE("contacts/{contact}/emails/{email}")
    suspend fun deleteEmail(
        @Path("contact") ulid: String,
        @Path("email") emailId: String,
    ): Response<Unit>

    @POST("contacts/{contact}/urls")
    suspend fun createUrl(@Path("contact") ulid: String, @Body body: ContactUrlRequest): JsonElement

    @PUT("contacts/{contact}/urls/{url}")
    suspend fun updateUrl(
        @Path("contact") ulid: String,
        @Path("url") urlId: String,
        @Body body: ContactUrlRequest,
    ): JsonElement

    @DELETE("contacts/{contact}/urls/{url}")
    suspend fun deleteUrl(
        @Path("contact") ulid: String,
        @Path("url") urlId: String,
    ): Response<Unit>

    @POST("contacts/{contact}/notes")
    suspend fun createNote(@Path("contact") ulid: String, @Body body: ContactNoteRequest): JsonElement

    @PUT("contacts/{contact}/notes/{note}")
    suspend fun updateNote(
        @Path("contact") ulid: String,
        @Path("note") noteId: String,
        @Body body: ContactNoteRequest,
    ): JsonElement

    @DELETE("contacts/{contact}/notes/{note}")
    suspend fun deleteNote(
        @Path("contact") ulid: String,
        @Path("note") noteId: String,
    ): Response<Unit>

    @POST("contacts/{contact}/dates")
    suspend fun createDate(@Path("contact") ulid: String, @Body body: ContactDateRequest): JsonElement

    @PUT("contacts/{contact}/dates/{date}")
    suspend fun updateDate(
        @Path("contact") ulid: String,
        @Path("date") dateId: String,
        @Body body: ContactDateRequest,
    ): JsonElement

    @DELETE("contacts/{contact}/dates/{date}")
    suspend fun deleteDate(
        @Path("contact") ulid: String,
        @Path("date") dateId: String,
    ): Response<Unit>

    @POST("contacts/{contact}/addresses")
    suspend fun createAddress(@Path("contact") ulid: String, @Body body: ContactAddressRequest): JsonElement

    @PUT("contacts/{contact}/addresses/{address}")
    suspend fun updateAddress(
        @Path("contact") ulid: String,
        @Path("address") addressId: String,
        @Body body: ContactAddressRequest,
    ): JsonElement

    @DELETE("contacts/{contact}/addresses/{address}")
    suspend fun deleteAddress(
        @Path("contact") ulid: String,
        @Path("address") addressId: String,
    ): Response<Unit>

    @POST("contacts/{contact}/calls")
    suspend fun createCall(@Path("contact") ulid: String, @Body body: ContactCallRequest): JsonElement

    @PUT("contacts/{contact}/calls/{call}")
    suspend fun updateCall(
        @Path("contact") ulid: String,
        @Path("call") callId: String,
        @Body body: ContactCallRequest,
    ): JsonElement

    @DELETE("contacts/{contact}/calls/{call}")
    suspend fun deleteCall(
        @Path("contact") ulid: String,
        @Path("call") callId: String,
    ): Response<Unit>

    @POST("contacts/{contact}/gift-ideas")
    suspend fun createGiftIdea(@Path("contact") ulid: String, @Body body: ContactGiftIdeaRequest): JsonElement

    @PUT("contacts/{contact}/gift-ideas/{gift_idea}")
    suspend fun updateGiftIdea(
        @Path("contact") ulid: String,
        @Path("gift_idea") giftId: String,
        @Body body: ContactGiftIdeaRequest,
    ): JsonElement

    @DELETE("contacts/{contact}/gift-ideas/{gift_idea}")
    suspend fun deleteGiftIdea(
        @Path("contact") ulid: String,
        @Path("gift_idea") giftId: String,
    ): Response<Unit>

    @GET("contacts/{contact}/comments")
    suspend fun listComments(@Path("contact") ulid: String): ContactCommentsResponse

    @POST("contacts/{contact}/comments")
    suspend fun createComment(
        @Path("contact") ulid: String,
        @Body body: ContactCommentStoreRequest,
    ): JsonElement

    @PUT("contacts/{contact}/comments/{comment}")
    suspend fun updateComment(
        @Path("contact") ulid: String,
        @Path("comment") commentId: String,
        @Body body: ContactCommentUpdateRequest,
    ): JsonElement

    @DELETE("contacts/{contact}/comments/{comment}")
    suspend fun deleteComment(
        @Path("contact") ulid: String,
        @Path("comment") commentId: String,
    ): Response<Unit>
}
