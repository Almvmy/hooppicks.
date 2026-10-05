import { ViewTransition } from "react";

// template (et pas layout) : il est remonté à chaque navigation, donc l'ancienne
// page "sort" et la nouvelle "entre", ce qui déclenche les animations
// page-out/page-in. Dans le layout, le contenu serait juste mis à jour.
// default="none" : pas d'animation au premier chargement ni sur les
// transitions qui ne sont pas des navigations.
export default function AppTemplate({ children }: { children: React.ReactNode }) {
  return (
    <ViewTransition enter="page-in" exit="page-out" default="none">
      <div>{children}</div>
    </ViewTransition>
  );
}
