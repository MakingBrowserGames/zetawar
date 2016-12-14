(ns zetawar.components.router
  (:require
   [cljs.core.async :as async]
   [com.stuartsierra.component :as component]
   [zetawar.players :as players]
   [zetawar.router :as router]))

(defrecord Router [datascript timbre conn ev-chan notify-chan notify-pub players]
  component/Lifecycle
  (start [component]
    (let [{:keys [conn]} datascript
          notify-pub (async/pub notify-chan #(nth % 1))]
      (router/start {:ev-chan ev-chan
                     :notify-chan notify-chan
                     :notify-pub notify-pub
                     :conn conn
                     :players players})
      (assoc component :conn (:conn datascript) :notify-pub notify-pub)))
  (stop [component]
    (async/close! ev-chan)
    (async/close! notify-chan)
    (doseq [player players]
      (players/stop player))
    (assoc component
           :datascript nil
           :timbre nil
           :conn nil
           :ev-chan nil
           :notify-chan nil
           :notify-pub nil
           :players nil)))

(defn new-router
  ([]
   ;; TODO: size 10 dropping-buffer is arbitrary. is there a better way?
   (new-router (async/chan (async/dropping-buffer 10))
               (async/chan)
               (atom {})))
  ([ev-chan notify-chan players]
   (component/using (map->Router {:ev-chan ev-chan
                                  :notify-chan notify-chan
                                  :players players})
                    [:datascript :timbre])))
