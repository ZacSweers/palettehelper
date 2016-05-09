![Palette Helper](art/feature.png)

<a href='https://play.google.com/store/apps/details?id=io.sweers.palettehelper&utm_source=global_co&utm_medium=prtnr&utm_content=Mar2515&utm_campaign=PartBadge&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'>
    <img alt='Get it on Google Play' 
         src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png'/>
</a>
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Palette%20Helper-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/1613)
[![Bugsnag](https://img.shields.io/badge/crash_reporting_by-bugsnag-green.png)](https://bugsnag.com/platforms/android)

Palette Helper is a simple utility app made to generate color palettes of images using Google's fantastic [Palette](https://developer.android.com/reference/android/support/v7/graphics/Palette.html) library. It's mostly a for-fun pet project, and intended as such for anyone that wants to see what kind of results the library churns out for images. I hope it might also be useful to any designers that want to design a color palette around a given scene or image.

The flow is pretty simple. You can either choose to open an image from storage, take one from your camera (if you feel like capturing a scene), or enter an image URL. You can also share an image with the app via the system's intent system either directly or via URL.

For fun, I opted to write this all in Kotlin. Assuming you install the Kotlin plugin, this should be easy to import and run in Android Studio.

Special thanks to [@emilsjolander](https://github.com/emilsjolander) for the icon/graphic designs!

License
-------

    Copyright 2015 Henri Z. Sweers

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
