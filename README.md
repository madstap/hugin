# hugin

![Odin with his ravens as depicted in an old manuscript](https://upload.wikimedia.org/wikipedia/commons/3/3c/Odin_hrafnar.jpg)

> Two ravens sit on his (Odin’s) shoulders and whisper all the news which they
> see and hear into his ear; they are called Huginn and Muninn. He sends them
> out in the morning to fly around the whole world, and by breakfast they
> are back again. Thus, he finds out many new things and this is why
> he is called ‘raven-god’ (hrafnaguð).

This is my preferred way of debugging clojure. It's a couple of simple tricks
wrapped in some functions for ergonomics and terseness.

[Introductory blog post](https://madstap.github.io/posts/dbg/).

## Usage

You can add this to a specific project, or like I do, add it to your
global `:user` profile.

Specific project: `foo/project.clj`
```clojure
(defproject foo ,,,
  :profiles {:dev {:dependencies [[madstap/hugin "0.1.5"]]}})
```

Globally: `~/.lein/profiles.clj`
```clojure
{:user {:plugins [,,,], :dependencies [[madstap/hugin "0.1.5"]]}}
```

Then use it in your namespace:

```clojure
(ns foo.core
  (:require
   ;; TODO: Remove this before committing.
   [hugin.dbg :as dbg]))
```

## Naming conventions

A name that ends with a `<` is meant to be wrapped around an expression,
intercepting the value returned while not changing the
meaning of the wrapped expression.

A name that ends in `<-` is the same, but with the arguments reversed so
it can be used from inside the `->` macro.
All functions with a `<` suffix has a `<-` equivalent.

## Things it has

### Printing

`dbg/p<` and `dbg/p<-` prints using `prn`.
`dbg/pp<` and `dbg/pp<-` uses `clojure.pprint/pprint`.

`dbg/p<` and `dbg/pp<` can take an optional message as the first argument,
so that if you have multiple expressions that you want printed you can distinguish
between them.

The `dbg/println-debugger` function takes a printing function and returns a
function like `dbg/p<`, so you can make your own.

### Debug atom

There's an atom called `dbg/a` with some functions wrapping it, for when printing
just isn't enough. It contains a map, so you can collect multiple
values at the same time.

`dbg/a<` works like `dbg/p<` only it puts stuff inside the atom.

In the same way `dbg/p<` has the signature `([x] [msg x])`, `dbg/a<`
has the signature `([x] [k x])`. The `k` is the key at which it will put
the value in the map inside `dbg/a`, and if you don't supply any it'll
default to `::dbg/default`.

For getting the values out again, there's the `dbg/a>` function. If call it
with no arguments, it'll get the value at `::dbg/default`, and if you supply
a key, it'll return the value at that key. You can also supply multiple keys,
in which case it'll return a tuple of values.

The important difference between using `dbg/a>` and accessing the atom directly
is when there's no value at `k`. `dbg/a>` will return `::dbg/nothing` instead of
`nil`, to distinguish between the value being `nil` and the  function not
getting called at all.

There's also `aconj<` which will put the value at `k` inside a vector. When
called again with the same `k` it will add the value to the end of that vector.
This is useful for collecting a series of values, from inside a looping
construct or similar.

`dbg/nuke` will clean out the contents of the atom,
for when you want to start over.

### Misc

`dbg/ppre` returns a hiccup :pre tag with it's argument pretty printed inside.

`def<` (signature `([name] [name expr])`) is like the inline `def` technique,
but it doesn't otherwise change the meaning of the code. If called with only
a name it defines a global with the same value as the local.

## License

Copyright © 2017 Aleksander Madland Stapnes

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
