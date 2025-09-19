import React, { useState, useEffect } from 'react';
import '../stylesheets/Modal.css';

const Modal = ({ show, onClose, title, inputs = [], onSubmit, submitLabel = 'Submit', cancelLabel = 'Cancel', children }) => {
    const initialFormData = inputs.reduce((acc, input) => ({ ...acc, [input.name]: input.value || '' }), {});
    const [formData, setFormData] = useState(initialFormData);

    useEffect(() => {
        if (show) {
            setFormData(initialFormData);
        }
    }, [show, inputs]);

    if (!show) return null;

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (onSubmit) {
            onSubmit(formData);
            setFormData(initialFormData);
        }
    };

    const handleClose = () => {
        setFormData(initialFormData);
        onClose();
    };

    return (
        <div className="modal">
            <div className="modal-content">
                {title && <h2 className="modal-title">{title}</h2>}
                {inputs.length > 0 ? (
                    <form onSubmit={handleSubmit}>
                        {inputs.map((input) => (
                            <div key={input.name} className="modal-input-group">
                                {input.label && <label className="modal-label">{input.label}</label>}
                                <input
                                    type={input.type || 'text'}
                                    name={input.name}
                                    value={formData[input.name]}
                                    onChange={handleChange}
                                    placeholder={input.placeholder}
                                    className="modal-input"
                                    min={input.min}
                                />
                            </div>
                        ))}
                        <div className="modal-footer">
                            <button type="submit" className="modal-button primary">{submitLabel}</button>
                            <button type="button" onClick={handleClose} className="modal-button">{cancelLabel}</button>
                        </div>
                    </form>
                ) : (
                    <>
                        {children}
                        <div className="modal-footer">
                            <button onClick={handleClose} className="modal-button">Close</button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
};

export default Modal;