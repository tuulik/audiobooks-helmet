(ns kirjasuositus.core
  (:gen-class)
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [dk.ative.docjure.spreadsheet :as docjure]))

(defn tee-finna-haku [kirjan-nimi]
  (client/get (str "https://api.finna.fi/api/v1/search?lookfor=" kirjan-nimi)))

(defn joku-äänikirja? [tulos]
  (seq (filter #(= "0/Sound/" (:value %)) (:formats tulos))))

(defn äänikirjat-hakutuloksesta [tulos]
  (filter joku-äänikirja? tulos))

(defn joku-helmet? [tulos]
  (seq (filter #(= "0/Helmet/" (:value %)) (:buildings tulos))))

(defn helmet-hakutuloksesta [tulos]
  (filter joku-helmet? tulos))

(defn muunna-hakutulos [tulos]
  (-> tulos
      :body
      cheshire/parse-string
      walk/keywordize-keys
      :records))

(defn tee-haku-ja-suodata-helmetin-äänikirjat [kirjan-nimi]
  (println "haetaan" kirjan-nimi)
  (-> kirjan-nimi
      tee-finna-haku
      muunna-hakutulos
      helmet-hakutuloksesta
      äänikirjat-hakutuloksesta))

(defn lue-kirjannimet-excelistä []
  (->> (docjure/load-workbook-from-resource "stories siivottu.xlsx")
       (docjure/select-sheet "Sheet1")
       (docjure/select-columns {:A :nimi})))

(defn lue-kirjat-excelistä-ja-tee-haku []
  (->> (lue-kirjannimet-excelistä)
       (map :nimi)
       (map tee-haku-ja-suodata-helmetin-äänikirjat)))
