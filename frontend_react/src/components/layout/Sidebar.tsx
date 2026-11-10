import { NavLink } from 'react-router-dom';

export default function Sidebar() {
    return (
        <aside className="sidebar">
            <h2>LiqFlow</h2>

            <nav className="sidebar-nav">
                <NavLink to="/dashboard">Dashboard</NavLink>
                <NavLink to="/inventory">Inventory</NavLink>
                <NavLink to="/products">Products</NavLink>
            </nav>
        </aside>
    );
}