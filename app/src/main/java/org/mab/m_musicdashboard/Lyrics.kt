package org.mab.m_musicdashboard

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
class Lyrics {
    var title: String = ""
    var artists: String = ""
    var url: String = ""

    constructor() {
    }

    constructor(title: String, artists: String, url: String) {
        this.title = title
        this.artists = artists
        this.url = url
    }

}