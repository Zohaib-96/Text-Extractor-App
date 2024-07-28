package com.example.textextracter

data class OCRResponse(
    val language: String,
    val orientation: String,
    val regions: List<Region>,
    val textAngle: Double
)
data class Region(
    val boundingBox: String,
    val lines: List<Line>
)

data class Word(
    val boundingBox: String,
    val text: String
)

data class Line(
    val boundingBox: String,
    val words: List<Word>
)