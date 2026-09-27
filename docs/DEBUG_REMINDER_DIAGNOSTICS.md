# Diagnostic des rappels : instrumentation DEBUG

Cette phase vise uniquement à observer le pipeline sans modifier le comportement applicatif.

## Journal de diagnostic

Les événements sont émis avec le tag `ReminderDebug` et n'incluent que :

- l'identifiant du rappel ;
- un statut ou un nom d'étape ;
- des champs de contexte minimaux (`dueTime`, `method`, `permission`, `channel`, `fallback`, etc.).

Aucune donnée utilisateur comme le titre, les notes ou le contenu de message n'est loggée.

## Chemin observé

1. `alarm.schedule.request`
2. `alarm.schedule.result`
3. `alarm.receiver.delivered`
4. `alarm.receiver.notification.start`
5. `notification.show.request`
6. `notification.foreground_service.start` / `notification.post.result`
7. `alarm.service.start`
8. `alarm.service.foreground`
9. `alarm.service.playback.prepare`
10. `alarm.service.playback.result`

## Procédure de reproduction

1. Démarrer l'application en mode debug.
2. Ouvrir le logcat filtré sur le tag `ReminderDebug` :
   `adb logcat -s ReminderDebug`
3. Créer un rappel avec une date future proche.
4. Vérifier le flux suivant dans l'ordre :
   - `alarm.schedule.request`
   - `alarm.schedule.result`
   - `alarm.receiver.delivered`
5. Si `alarm.schedule.result` n'apparaît pas, le rappel n'a jamais été programmé.
6. Si `alarm.receiver.delivered` n'apparaît pas, l'alarme n'a pas été délivrée par `AlarmManager`.
7. Si le receiver est vu mais `notification.post.result` est absent, le défaut est dans la création ou l'affichage de la notification.
8. Si `notification.foreground_service.start` apparaît mais `alarm.service.playback.result` manque, le problème est du côté du service audio / le son.
9. Si `notification.fullscreen.fallback` apparaît, l'accès Full-Screen est bloqué ou révoqué et le système bascule vers le fallback.

## Cas de test recommandés

- rappel unique normal ;
- exact alarm permission refusée ;
- mode avion puis réveil ;
- Doze / batterie optimisée ;
- reboot puis reprise de l'alarme ;
- notification style `FULL_SCREEN` ;
- notification style `HEADS_UP` ;
- rappel snoozé ;
- rappel complété / annulé.

## Interprétation

- `alarm.schedule.request` sans `alarm.schedule.result` = problème d'ordonnancement local.
- `alarm.schedule.result` sans `alarm.receiver.delivered` = problème `AlarmManager` / OS / Doze / permission exact alarm.
- `alarm.receiver.delivered` sans `notification.post.result` = problème de notification / permission / channel.
- `notification.foreground_service.start` sans `alarm.service.playback.result` = problème audio / service de fond.
- `notification.fullscreen.fallback` = blocage de `canUseFullScreenIntent()` ou revocation de l'accès.