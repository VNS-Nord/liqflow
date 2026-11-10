export interface Inventory {
    id: string;
    locationId: string;
    locationCode: string;
    productId: string;
    productSku: string;
    productName: string;
    quantity: number;
    reservedQuantity: number;
    availableQuantity: number;
    minThreshold: number;
}
