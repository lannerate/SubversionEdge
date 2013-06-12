/*
* CollabNet Subversion Edge
* Copyright (C) 2010, CollabNet Inc. All rights reserved.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
import java.util.Properties

includeTargets << grailsScript("_GrailsWar")
includeTargets << new File("scripts", "_CommonTargets.groovy")

/**
 * <p>May 8, 2012 6:18:37 PM</p> 
 * @param langFile is the file instance to a message properties.
 * @return an array of 2 elements where the first contains a map structure of the
 * following entry: [key:v, value:v, line:v]. The second is a line indexer by the
 * key.
 */
def loadAndIndexProperties(langFile) {
  if (!langFile.exists()) {
    println "The given file $langFile does not exit!"
    System.exit(1)
  }
  def lineProps = [] as LinkedList;
  def keyIndex = [:]
  langFile.eachLine { line ->
    if (!line.startsWith("#") && line.contains("=")) {
      def propKeyValue = line.split("=")
      // for the cases when there is no value assigned, maybe next line
      def key = propKeyValue[0].trim()
      def value = propKeyValue.length == 2 ? propKeyValue[1].trim() : ""
      def newLine = line.trim()
      lineProps << ["key": key, "value": value, "line": newLine]
      keyIndex[key] = value

    } else {
      lineProps << ["key": null, "value": null, "line": line.trim()]
      keyIndex[line] = line.trim()
    }
  }
  [lineProps, keyIndex]
}

/**
 * This script diffs ONLY THE KEYS of a chosen i18n properties with the English
 * one and updates the chosen file inline "message_LANGCODE.properties. Use regular
 * version-control diff for details. Note that the command will align the internationalized
 * version of the properties with the English one.
 * 
 * Run "grails diffI18n" and choose a language from the menu.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 */

target(build: 'Builds the distribution file structure') {
    setDefaultTarget("compile")

    distDir = "${basedir}/grails-app/i18n/"
    def en = "messages.properties"
    println ""
    println "####### CollabNet Subversion Edge Language Keys Diff Util ########"
    println "# Available Languages at '${distDir}'"

    def langIndex = [:]
    def localeIndex = [:]
    def counter = 0
    def langs = new File(distDir).listFiles()
    for (langFile in langs) {
        def fileName = langFile.canonicalPath
        if (fileName.contains(en) || (!fileName.startsWith("messages") &&
            !fileName.endsWith(".properties"))) {
            continue
        }

        int sep = fileName.lastIndexOf(File.separator);
        fileName = fileName.substring(sep+1, fileName.length())

        def langCode = fileName.replace("messages_", "").replace(".properties",
            "")
        def locale
        counter++
        if (langCode.contains("_")) {
            def langCountry = langCode.split("_")
            locale = new Locale(langCountry[0], langCountry[1])
            localeIndex[counter] = locale
        } else {
            locale = new Locale(langCode)
            localeIndex[counter] = locale
        }
        println "# " + counter + ") " + locale.getDisplayName() + " (${locale})"
        langIndex[counter] = langFile
    }

    def stdin = new BufferedReader(new InputStreamReader(System.in))
    print "# Choose one to diff with the English version: "
    String chosenLang = stdin.readLine()
    println "# You selected " + chosenLang + ". Diffing that language with " + 
        "English"
    println ""

    def propsEn = new Properties()
    new File(distDir + en).withReader { r ->
        propsEn.load(r)
    }

    def propsDiff = new Properties()
    def selectedLangFile = langIndex[new Integer(chosenLang)]
    selectedLangFile.withReader { r ->
        propsDiff.load(r)
    }

    def enLinesLang = loadAndIndexProperties(new File(distDir + en))
    def enLangLines = enLinesLang[0]
    def enKeyIndex = enLinesLang[1]

    def selectedLang = loadAndIndexProperties(selectedLangFile) 
    def selectedLangLines = selectedLang[0]
    def otherKeyIndex = selectedLang[1]

    if (selectedLangLines.size() > enLangLines.size()) {
      println "There are more properties in the Other language (${selectedLangLines.size()} vs ${enLangLines.size()})."

    } else if (selectedLangLines.size() < enLangLines.size()) {
      println "There are more properties in the English language (${enLangLines.size()} vs ${selectedLangLines.size()})."

    } else {
      println "Possibly the files have the same number of keys."
    }

    // TODO: this is not correct. Just play with the index and the non-null keys 
    def removedProps = selectedLangLines.findAll{ entry -> entry.key != null }
    def englishProps = enLangLines.findAll{ entry -> entry.key != null }
    //removedProps.removeAll(engKeys)
    int removed = removedProps.size()

    int added = 0

    def newPropLines = [] as LinkedList
    for (int i = 0; i < enLangLines.size(); i++) {
      def englishEntry = enLangLines.get(i)
      def otherEntry = null;
      try {
        otherEntry = selectedLangLines.get(i)

      } catch (IndexOutOfBoundsException e) {
        otherEntry = null
      }

      // add any line without key
      if (!englishEntry.key) {
        newPropLines << englishEntry.line
        ++added
        continue
      }

      // add the line with an existing translation
      if (otherKeyIndex.containsKey(englishEntry.key)) {
        newPropLines << englishEntry.key + "=" + otherKeyIndex[englishEntry.key]
        continue

      } else {
        newPropLines << englishEntry.line
      }
    }

    def chosenLocale = localeIndex[new Integer(chosenLang)]
    def langName = chosenLocale.displayName
    println "# The language '${langName}' is now updated!"

    def code = localeIndex[new Integer(chosenLang)]
    def reprt = new File(distDir + "messages_${code}.properties")
    reprt.withWriter('UTF-8') {
        for (entry in newPropLines) {
          it.writeLine entry
        }
    }
    if (added > 0) {
        println "# The language '${langName}' needs more has $added new " +
            "string" + (added == 1 ? '' : 's')
    }
    if (removed > 0) {
        println "# $removed key" +
            (removed == 1 ? ' was' : 's were') + " removed from " + 
            "the language '${langName}'."
    }
    println "# Remember to edit your language '${langName}' using an editor " +
        "in the format UTF-8."
}
setDefaultTarget("build")