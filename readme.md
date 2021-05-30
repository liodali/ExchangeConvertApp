# Exchange Rate application
> Android application build it  with kotlin that convert amount (selected currency) to other currencies

## Build
* should use Android Studio 2020.3.1 beta 2 or above

##Notice
```
I used this api [https://api.exchangerate.host/] instead [https://currencylayer.com/]
because they change their policy and free key
doesn't give me  the possibility to change base currency to get latest rates
```
##### In this project, we implement the  clean architecture
* we have 3 layer:

  * <srong>App module </string>  : This module contains all of the code related to the UI/Presentation layer such as activities,fragment,dialog,custom views  and contain viewmodel,dependency injection module app
  * <srong>Core</string> : holds all concrete implementations of our repositories,usecaes and other data sources like  network
  * <srong>Domain module </string>  : contain all interfaces of repositories ,usecase and data classes


> I combined jetpack compose (UI toolkit) with xml to build user interfaces
 
> I used hilt as dependency injection for this project

> I used retrofit for http calls and flowAPI to collect data


