const BASE_URL = "http://localhost:8080";

export function iconUrl(category: string, id: string): string {
  return `${BASE_URL}/api/icon/${category}/${id}`;
}
