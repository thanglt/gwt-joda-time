## Summary ##

Port of [JodaTime](http://joda-time.sourceforge.net/) for use with the [Google Web Toolkit (GWT)](http://code.google.com/webtoolkit/overview.html).

## What's Different from JodaTime ##

GWT only supports a subset of available Java classes (see [http://code.google.com/webtoolkit/doc/latest/RefJreEmulation.html](http://code.google.com/webtoolkit/doc/latest/RefJreEmulation.html) for details). Thus, certain features of JodaTime are not supported as this point. The following classes are not supported by GWT and so any related code has been removed from `gwt-joda-time`:

  1. [java.io.Writer](http://java.sun.com/javase/6/docs/api/java/io/Writer.html)
  1. [java.util.Calendar](http://java.sun.com/javase/6/docs/api/java/util/Calendar.html) - including [LocalDate](http://joda-time.sourceforge.net/apidocs/org/joda/time/LocalDate.html)
  1. [java.io.ObjectInputStream](http://java.sun.com/javase/6/docs/api/java/io/ObjectInputStream.html) and [java.io.ObjectOutputStream](http://java.sun.com/javase/6/docs/api/java/io/ObjectOutputStream.html)