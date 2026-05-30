import { backendPortPromise } from "@/lib/api";

let backendPort = 8080;
void backendPortPromise.then(p => { backendPort = p; });

export function iconUrl(category: string, id: string): string {
  return `http://localhost:${backendPort}/api/icon/${category}/${id}`;
}
