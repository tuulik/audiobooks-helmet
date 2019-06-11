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

(defn kirjan-nimi-tulokseen [tulos]
  (->> tulos
       (map :title)
       (str/join "/")))

(defn poista-tyhjät [tulos]
  (remove empty? tulos))

(defn kirjoita-exceliin! [kirjannimet]
  (let [wb (docjure/create-workbook "Sheet1" kirjannimet)]
    (docjure/save-workbook! "äänikirjat.xlsx" wb)))

(defn tee-haku-ja-suodata-helmetin-äänikirjat! [kirjan-nimi]
  (println "haetaan" kirjan-nimi)
  (-> kirjan-nimi
      tee-finna-haku
      muunna-hakutulos
      helmet-hakutuloksesta
      äänikirjat-hakutuloksesta
      kirjan-nimi-tulokseen))

(defn tee-haku-ja-hae-avainsanat! [kirjan-nimi]
  (println "haetaan" kirjan-nimi)
  (->> kirjan-nimi
       tee-finna-haku
       muunna-hakutulos
       (map :subjects)))

(defn lue-kirjannimet-excelistä []
  (->> (docjure/load-workbook-from-resource "stories siivottu.xlsx")
       (docjure/select-sheet "Sheet1")
       (docjure/select-columns {:A :nimi})))

(defn lue-kirjat-excelistä-ja-hae-äänikirjat! []
  (->> (lue-kirjannimet-excelistä)
       (map :nimi)
       (map tee-haku-ja-suodata-helmetin-äänikirjat!)
       poista-tyhjät
       (map vector)
       kirjoita-exceliin!))

(defn järjestä-arvoilla [tulokset]
  #_(println "järjestä-arvoilla" tulokset)
  (into (sorted-map-by (fn [avain1 avain2]
                         (compare [(get tulokset avain2) avain2]
                                  [(get tulokset avain1) avain1])))
        tulokset))

(defn lue-kirjat-excelistä-ja-hae-avainsanat! []
  (->> (lue-kirjannimet-excelistä)
       (map :nimi)
       (map tee-haku-ja-hae-avainsanat!)
       flatten
       (frequencies)
       (järjestä-arvoilla)))
