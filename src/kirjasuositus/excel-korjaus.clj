(ns kirjasuositus.excel-korjaus
  (:gen-class)
  (:require [clojure.string :as str]
            [dk.ative.docjure.spreadsheet :as docjure]))

(defn b-sarake-excelistä []
  (->> (docjure/load-workbook-from-resource "stories excel.xlsx")
       (docjure/select-sheet "Sheet1")
       (docjure/select-columns {:B :nimi})))

(defn sisältää-kirjailijan-nimen? [solun-teksti]
  (str/starts-with? solun-teksti "by "))

(defn kirjannimet-excelistä []
  (->> (b-sarake-excelistä)
       (map :nimi)
       (remove nil?)
       (remove sisältää-kirjailijan-nimen?)
       (map vector)))

(defn luo-excel! [kirjannimet]
  (let [wb (docjure/create-workbook "Sheet1" kirjannimet)]
    (docjure/save-workbook! "stories siivottu.xlsx" wb)))

(defn lue-kirjannimet-ja-kirjoita-exceliin []
  (luo-excel! (kirjannimet-excelistä)))
