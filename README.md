# VerseKeeper-Public


![Header](/imgs/cover.png)

## Summary:
I created VerseKeeper to fulfill every feature I want in a Bible memorization app. It is currently under development, and it is a private repository. However, I wanted to highlight some of my code from this app that is most transferable to skills needed for a data engineer.

So, I created this separate repository to include some screenshots of the following topics:
- [VerseKeeper-Public](#versekeeper-public)
  - [Summary:](#summary)
  - [Web Scraping:](#web-scraping)
    - [Sample Code:](#sample-code)
  - [User Authentication:](#user-authentication)
    - [Sample Code:](#sample-code-1)
  - [API Access:](#api-access)
    - [Sample Code:](#sample-code-2)

&nbsp;

## Web Scraping:
I always enjoy reading Bible commentary while I am memorizing Scripture. So, VerseKeeper uses [jsoup](https://jsoup.org/) to scrape commentary from BibleRef. The BibleRef website allows their Bible commentary to be re-distributed under specific guidelines, which I have judiciously followed. 
 
Here is an example VerseKeeper's web scraping in action:

[![VerseKeeper: Web Scraping](/imgs/commentary_single-verse_play.png)](https://www.youtube.com/watch?v=bGydo8aoIF8)

&nbsp;

### Sample Code:
As mentioned earlier, VerseKeeper uses jsoup for web scraping. I have included a sample of the [GetVerseCommentaryTask class](sample_web-scrape.java) in this repository.


&nbsp;

## User Authentication:
A previous API I was using required that I track user sessions anonymously. I also plan to offer users backup capability. So, I integrated the [Firebase API](https://firebase.google.com/) into VerseKeeper. Currently, users can only sign in with a Google account, but I plan to add more Identity Providers.

Below is an example of signing into Google:
[![VerseKeeper: User Authentication](/imgs/google_sign-in_play.png)](https://youtube.com/shorts/Wac63O3hBrE)

### Sample Code:
Here is a sample of the [Firebase Authentication](Sample_FirebaseAuth.java) code.

&nbsp;

## API Access:
Besides using the Firebase API, VerseKeeper also used API from [API.Bible](https://scripture.api.bible/livedocs) in a previous version to import Bible verses selected by the user. I abandoned this feature when I found out the licensing costs of including multiple translations would be prohibitive.

&nbsp;

### Sample Code:
I have included a sample of the API interface calls below:
- [Retrofit2 Intefrace Sample](Sample_API_Retrofit2_Interface.java)
- [Retrofit2 Calls Sample](Sample_API_Retrofit_Calls.java)