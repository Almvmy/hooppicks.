// L'App Router exécute la version canary de React embarquée par Next, qui
// exporte <ViewTransition>. Les types stables de @types/react ne la déclarent
// pas : on charge les types canary pour que l'import compile.
/// <reference types="react/canary" />
