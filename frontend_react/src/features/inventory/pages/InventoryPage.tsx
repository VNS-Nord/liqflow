import {useEffect, useState} from "react";
import {getInventories} from "../api/inventoryApi";
import type {Inventory} from "../types";

export default function InventoryPage() {
    const [inventories, setInventories] = useState<Inventory[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        getInventories()
            .then(page => setInventories(page.content))
            .catch(() => setError('Failed to load inventory.'))
            .finally(() => setLoading(false));
    }, []);

    if (loading) {
        return <p>Loading inventory...</p>;
    }

    if (error) {
        return <p role="alert">{error}</p>
    }

    return (
        <div>
            <h1>Inventory</h1>

            <table>
                <thead>
                <tr>
                    <th>SKU</th>
                    <th>Product</th>
                    <th>Location</th>
                    <th>Quantity</th>
                    <th>Reserved</th>
                    <th>Available</th>
                    <th>Min. threshold</th>
                </tr>
                </thead>

                <tbody>
                {inventories.map(inventory => (
                    <tr key={inventory.id}>
                        <td>{inventory.productSku}</td>
                        <td>{inventory.productName}</td>
                        <td>{inventory.locationCode}</td>
                        <td>{inventory.quantity}</td>
                        <td>{inventory.reservedQuantity}</td>
                        <td>{inventory.availableQuantity}</td>
                        <td>{inventory.minThreshold}</td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
}