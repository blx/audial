# audial

Provides an efficient interface for telling the computer to play songs.

Yes, this is a simple concept, but I'm really lazy. Anyway, the goal is to provide a very low-friction search interface that queries both the local iTunes library and Spotify, can find songs by lyrics as well as more prosaic properties like title or artist, and then makes the music happen.

Currently Audial serves up a browser-based, song-searchin' interface right at home
on localhost:8080.
Future plans involve desktopifying with Electron and then binding a systemwide
hotkey to instantly bring up the search interface, analogous to cmd-space for Spotlight.

The API and library structure are highly volatile at the moment. Exploratory, rather.
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

**Electron frontend**

```shell
gem install foreman
npm install electron-prebuilt
ln -s node_modules/electron-prebuilt/dist/Electron.app/Contents/MacOS/Electron electron

foreman start &
./electron .
```


## License

Copyright © 2015 Ben Cook

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
