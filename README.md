# PPC_Leader_Election

Algorithme d'élection de leader dans un réseau complet.

## Exécution

Pour lancer les noeuds, Nous exécutons dans des terminaux (ou onglets) différents les commandes suivantes:

 ```shell
 sbt "run 0"
 sbt "run 1"
 sbt "run 2"
 sbt "run 3"
 ```

> Nous estimons que le premier leader est 0, donc nous devons commençer par lancer le noeud 0 afin que le système fonctionne correctement.  
>
> Nous devons lancer le noeud 0 (au moins une fois) pour que le mécanisme de l'élection de leader commence à fonctionner. 
>
> Idéalement, nous commençons par lancer le noeud 0.
