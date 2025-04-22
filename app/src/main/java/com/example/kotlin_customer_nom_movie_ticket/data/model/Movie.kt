package com.example.kotlin_customer_nom_movie_ticket.data.model

data class Movie(
    val movie_id: String = "",
    val title: String = "",
    val genre: String = "",
    val language: String = "",
    val country: String = "",
    val release_year: Int = 0,
    val duration: Int = 0,
    val status: MovieStatus = MovieStatus.isShowing,
    val synopsis: String = "",
    val poster_url: String = "",
    val trailer_url: String = "",
    val director_id: String = "",
    val age_rating: String = "",
    val banner: String = "",
    val actor_ids: List<String> = emptyList(),

    val ratings: MovieRating = MovieRating()
)

enum class MovieStatus {
    isShowing,
    isComing,
    isClosed
}

data class MovieRating(
    val total_score: Int = 0,
    val total_votes: Int = 0,
    val average_rating: Float = 0f,
    val ratings_detail: Map<Int, Int> = emptyMap()
)
