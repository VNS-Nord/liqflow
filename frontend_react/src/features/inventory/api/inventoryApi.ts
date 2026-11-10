import type { Inventory } from '../types';
import type { Page } from '../../../shared/types/pagination';

const API_BASE = "/api/v1/inventories";

export async function getInventories(
    page: number = 0,
    size: number = 10
): Promise<Page<Inventory>> {
    const response = await fetch(`${API_BASE}?page=${page}&size=${size}`, {
        credentials: "include"
    });

    if (!response.ok) {
        throw new Error(`Failed to load inventory ${response.status}`)
    }

    return response.json();
}