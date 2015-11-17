# audial

Provides an efficient interface for telling the computer to play songs.

This is a very simple concept, yes, but as a Millennial(tm) I *Want Things Now*(tm).
Or something. Anyway, the goal is to provide a very low-friction search interface
that queries both the local iTunes library and Spotify, can find songs by lyrics as
well as more prosaic properties like title or artist, and then makes the music happen.

Currently Audial serves up a browser-based, song-searchin' interface right at home
on localhost:8080.
Future plans involve desktopifying with Electron and then binding a systemwide
hotkey to instantly bring up the search interface, analogous to cmd-space for Spotlight.

The API and library structure are heavily volatile at the moment. Exploratory, rather.
That's a good word.

## Installation

Installation instructions are a #TODO, sorry. I just ask Leiningen to do things.

Note that I've been developing on fairly recent versions of OS X and iTunes, so
older versions may be wonky.

## Configuration

Create or update `profiles.clj` based on the following so the right
environment variables are defined (keep your spotify credentials out of git):

```clojure
{:local-dev {:env {:itunes-file "/Users/XXX/Music/iTunes/iTunes Library.xml"
                   :file-prefix "/Users/XXX/Music/iTunes/iTunes Media/"

                   :spotify-client-id "XXX"
                   :spotify-client-secret "XXX"}}}
```

## Usage

    lein ring server-headless

and elsewhere, if using the clojurescript frontend:

    lein cljsbuild auto dev


## License

Copyright © 2015 Ben Cook

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
