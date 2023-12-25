# Exchange Rate application
> Android application build it  with kotlin that convert amount (selected currency) to other currencies

## Build
* should use Android Studio 2023.1.1 or above
* compose 1.5.7/kotlin 1.9.21

##Notice
```
I used this api [https://api.exchangerate.host/] instead [https://currencylayer.com/]
because they change their policy and free key
doesn't give me  the possibility to change base currency to get latest rates
```
##### In this project, we implement the  clean architecture
* we have 3 layer:

  * <srong>App module </string>  : This module contains all of the code related to the UI/Presentation layer such as activities,compose component,dialog and contain ViewModel,dependency injection module app
  * <srong>Core</string> : holds all concrete implementations of our repositories and other data sources like  network
  * <srong>Domain module </string>  : contain all interfaces of repositories and data classes


> I combined jetpack compose (UI toolkit) with xml to build user interfaces
 
> I used koin as dependency injection for this project

> I used retrofit for http calls and flowAPI to collect data


